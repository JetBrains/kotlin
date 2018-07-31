@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import java.io.File

class PFile(val path: File, val text: String) {
    fun write() {
        path.parentFile.mkdirs()
        path.writeText(text)
    }
}

fun PFile(path: File, xml: xml) = PFile(path, xml.toString())

fun render(project: PProject): List<PFile> {
    val files = mutableListOf<PFile>()

    files += renderModulesFile(project)
    project.modules.forEach { files += renderModule(project, it) }
    project.libraries.forEach { files += renderLibrary(project, it) }

    return files
}

private fun renderModulesFile(project: PProject) = PFile(
    File(project.rootDirectory, ".idea/modules.xml"),
    xml("project", "version" to 4) {
        xml("component", "name" to "ProjectModuleManager") {
            xml("modules") {
                val pathContext = ProjectContext(project)

                for (module in project.modules) {
                    val moduleFilePath = pathContext(module.moduleFile)

                    if (module.group != null) {
                        xml("module", "fileurl" to "file://$moduleFilePath", "filepath" to moduleFilePath, "group" to module.group)
                    } else {
                        xml("module", "fileurl" to "file://$moduleFilePath", "filepath" to moduleFilePath)
                    }
                }
            }
        }
    }
)

private fun renderModule(project: PProject, module: PModule) = PFile(
    module.moduleFile,
    xml("module",
        "type" to "JAVA_MODULE",
        "version" to 4
    ) {
        val moduleForProductionSources = module.moduleForProductionSources
        if (moduleForProductionSources != null) {
            xml("component", "name" to "TestModuleProperties", "production-module" to moduleForProductionSources.name)
        }

        val kotlinCompileOptionsList = module.contentRoots.flatMap { it.sourceRoots }.mapNotNull { it.kotlinOptions }
        var kotlinCompileOptions = kotlinCompileOptionsList.firstOrNull()
        for (otherOptions in kotlinCompileOptionsList.drop(1)) {
            kotlinCompileOptions = kotlinCompileOptions?.intersect(otherOptions)
        }

        val pathContext = ModuleContext(project, module)

        val platformVersion = (kotlinCompileOptions?.jvmTarget ?: "1.8")
        val classesDirectory = File(project.rootDirectory, "out/production/${module.name}")

        if (kotlinCompileOptions != null) {
            xml("component", "name" to "FacetManager") {
                xml("facet", "type" to "kotlin-language", "name" to "Kotlin") {
                    xml("configuration", "version" to 3, "platform" to "JVM $platformVersion", "useProjectSettings" to "false") {
                        xml("compilerSettings") {
                            xml(
                                "option",
                                "name" to "additionalArguments",
                                "value" to kotlinCompileOptions.extraArguments.joinToString(" ")
                            )
                        }
                        xml("compilerArguments") {
                            xml("option", "name" to "destination", "value" to pathContext(classesDirectory))

                            fun Any?.option(name: String) {
                                if (this != null) xml("option", "name" to name, "value" to this.toString())
                            }

                            kotlinCompileOptions.noStdlib.option("noStdlib")
                            kotlinCompileOptions.noReflect.option("noReflect")
                            kotlinCompileOptions.moduleName.option("moduleName")
                            kotlinCompileOptions.languageVersion.option("languageVersion")
                            kotlinCompileOptions.apiVersion.option("apiVersion")
                            kotlinCompileOptions.addCompilerBuiltIns.option("addCompilerBuiltIns")
                            kotlinCompileOptions.loadBuiltInsFromDependencies.option("loadBuiltInsFromDependencies")

                            xml("option", "name" to "pluginOptions") { xml("array") }
                            xml("option", "name" to "pluginClasspaths") { xml("array") }
                        }
                    }
                }
            }
        }

        xml("component",
            "name" to "NewModuleRootManager",
            "LANGUAGE_LEVEL" to "JDK_${platformVersion.replace('.', '_')}",
            "inherit-compiler-output" to "true"
        ) {
            xml("exclude-output")

            for (contentRoot in module.contentRoots) {
                xml("content", pathContext.url(contentRoot.path)) {
                    for (sourceRoot in contentRoot.sourceRoots) {
                        var args = arrayOf(pathContext.url(sourceRoot.path))

                        args += when (sourceRoot.kind) {
                            PSourceRoot.Kind.PRODUCTION -> ("isTestSource" to "false")
                            PSourceRoot.Kind.TEST -> ("isTestSource" to "true")
                            PSourceRoot.Kind.RESOURCES -> ("type" to "java-resource")
                            PSourceRoot.Kind.TEST_RESOURCES -> ("type" to "java-test-resource")
                        }

                        xml("sourceFolder", *args)
                    }

                    for (excludedDir in contentRoot.excludedDirectories) {
                        xml("excludeFolder", pathContext.url(excludedDir))
                    }
                }
            }

            xml("orderEntry", "type" to "inheritedJdk")

            xml("orderEntry", "type" to "sourceFolder", "forTests" to "false")

            for (orderRoot in module.orderRoots) {
                val dependency = orderRoot.dependency

                var args = when (dependency) {
                    is PDependency.ModuleLibrary -> arrayOf(
                        "type" to "module-library"
                    )
                    is PDependency.Module -> arrayOf(
                        "type" to "module",
                        "module-name" to dependency.name
                    )
                    is PDependency.Library -> arrayOf(
                        "type" to "library",
                        "name" to dependency.name,
                        "level" to "project"
                    )
                }

                if (dependency is PDependency.Module && orderRoot.isProductionOnTestDependency) {
                    args += ("production-on-test" to "")
                }

                args += ("scope" to orderRoot.scope.toString())
                if (orderRoot.isExported) {
                    args += ("exported" to "")
                }

                xml("orderEntry", *args) {
                    if (dependency is PDependency.ModuleLibrary) {
                        add(renderLibraryToXml(dependency.library, pathContext, named = false))
                    }
                }
            }
        }
    }
)

private fun renderLibrary(project: PProject, library: PLibrary): PFile {
    val pathContext = ProjectContext(project)

    // TODO find how IDEA escapes library names
    val escapedName = library.renderName().replace(" ", "_").replace(".", "_").replace("-", "_")

    return PFile(
        File(project.rootDirectory, ".idea/libraries/$escapedName.xml"),

        xml("component", "name" to "libraryTable") {
            add(renderLibraryToXml(library, pathContext))
        })
}

private fun renderLibraryToXml(library: PLibrary, pathContext: PathContext, named: Boolean = true): xml {
    val args = if (named) arrayOf("name" to library.renderName()) else emptyArray()

    return xml("library", *args) {
        xml("CLASSES") {
            library.classes.forEach { xml("root", pathContext.url(it)) }
        }

        xml("JAVADOC") {
            library.javadoc.forEach { xml("root", pathContext.url(it)) }
        }

        xml("SOURCES") {
            library.sources.forEach { xml("root", pathContext.url(it)) }
        }
    }
}

fun PLibrary.renderName() = name?.takeIf { it != "unspecified" } ?: classes.first().nameWithoutExtension