@file:Suppress("unused") // usages in build scripts are not tracked properly
@file:JvmName("RepoArtifacts")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.JAVADOC_ELEMENTS_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPlugin.SOURCES_ELEMENTS_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import plugins.KotlinBuildPublishingPlugin
import plugins.mainPublicationName
import java.io.File


private const val MAGIC_DO_NOT_CHANGE_TEST_JAR_TASK_NAME = "testJar"

fun Project.testsJar(body: Jar.() -> Unit = {}): Jar {
    val testsJarCfg = configurations.getOrCreate("tests-jar").extendsFrom(configurations["testApi"])

    return task<Jar>(MAGIC_DO_NOT_CHANGE_TEST_JAR_TASK_NAME) {
        dependsOn("testClasses")
        pluginManager.withPlugin("java") {
            from(testSourceSet.output)
        }
        archiveClassifier.set("tests")
        body()
        project.addArtifact(testsJarCfg, this, this)
    }
}

fun Project.setPublishableArtifact(
    jarTask: TaskProvider<out Jar>
) {
    addArtifact("runtimeElements", jarTask)
    addArtifact("apiElements", jarTask)
    addArtifact("archives", jarTask)
}

fun removeJarTaskArtifact(
    jarTask: TaskProvider<out Jar>
): Configuration.() -> Unit = {
    val jarFile = jarTask.get().archiveFile.get().asFile
    artifacts.removeIf { it.file == jarFile }
}

fun Project.noDefaultJar() {
    val jarTask = tasks.named<Jar>("jar") {
        enabled = false
    }

    configurations.named("apiElements", removeJarTaskArtifact(jarTask))
    configurations.named("runtimeElements", removeJarTaskArtifact(jarTask))
    configurations.named("archives", removeJarTaskArtifact(jarTask))
}

@JvmOverloads
fun Jar.addEmbeddedRuntime(embeddedConfigurationName: String = "embedded") {
    project.configurations.findByName(embeddedConfigurationName)?.let { embedded ->
        dependsOn(embedded)
        val archiveOperations = project.serviceOf<ArchiveOperations>()
        from {
            embedded.map { dependency: File ->
                check(!dependency.path.contains("kotlin-stdlib")) {
                    """
                    |There's an attempt to have an embedded kotlin-stdlib in $project which is likely a misconfiguration
                    |All embedded dependencies:
                    |    ${embedded.files.joinToString(separator = "\n|    ")}
                    """.trimMargin()
                }

                if (dependency.extension.equals("jar", ignoreCase = true)) {
                    archiveOperations.zipTree(dependency)
                } else {
                    dependency
                }
            }
        }
    }
}

fun Project.runtimeJar(body: Jar.() -> Unit = {}): TaskProvider<out Jar> {
    val jarTask = tasks.named<Jar>("jar")
    jarTask.configure {
        addEmbeddedRuntime()
        setupPublicJar(project.extensions.getByType<BasePluginExtension>().archivesName.get())
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        body()
    }

    return jarTask
}

fun Project.runtimeJarWithRelocation(body: ShadowJar.() -> Unit = {}): TaskProvider<out Jar> {
    noDefaultJar()

    val shadowJarTask = tasks.register<ShadowJar>("shadowJar") {
        archiveClassifier.set("shadow")
        configurations = configurations + listOf(project.configurations["embedded"])
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        body()
    }

    val runtimeJarTask = tasks.register<Jar>("runtimeJar") {
        dependsOn(shadowJarTask)
        from {
            zipTree(shadowJarTask.get().outputs.files.singleFile)
        }
        setupPublicJar(project.extensions.getByType<BasePluginExtension>().archivesName.get())
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    project.addArtifact("archives", runtimeJarTask, runtimeJarTask)
    project.addArtifact("runtimeElements", runtimeJarTask, runtimeJarTask)
    project.addArtifact("apiElements", runtimeJarTask, runtimeJarTask)

    return runtimeJarTask
}

fun Project.runtimeJar(task: TaskProvider<ShadowJar>, body: ShadowJar.() -> Unit = {}): TaskProvider<out Jar> {

    noDefaultJar()

    task.configure {
        configurations = configurations + listOf(project.configurations["embedded"])
        setupPublicJar(project.extensions.getByType<BasePluginExtension>().archivesName.get())
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        body()
    }

    project.addArtifact("archives", task, task)
    project.addArtifact("runtimeElements", task, task)
    project.addArtifact("apiElements", task, task)

    return task
}

private fun Project.mainJavaPluginSourceSet() = findJavaPluginExtension()?.sourceSets?.findByName("main")
private fun Project.mainKotlinSourceSet() =
    (extensions.findByName("kotlin") as? KotlinSourceSetContainer)?.sourceSets?.findByName("main")
private fun Project.sources() = mainJavaPluginSourceSet()?.allSource ?: mainKotlinSourceSet()?.kotlin

@JvmOverloads
fun Project.sourcesJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> {
    configure<JavaPluginExtension> {
        withSourcesJar()
    }

    val sourcesJar = getOrCreateTask<Jar>("sourcesJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("sources")

        from(project.sources())
        addEmbeddedSources()

        body()
    }

    addArtifact("archives", sourcesJar)
    addArtifact("sources", sourcesJar)

    configurePublishedComponent {
        addVariantsFromConfiguration(configurations[SOURCES_ELEMENTS_CONFIGURATION_NAME]) { }
    }

    return sourcesJar
}

/**
 * Also embeds into final '-sources.jar' file source files from embedded dependencies.
 */
fun Project.sourcesJarWithSourcesFromEmbedded(
    vararg embeddedDepSourcesJarTasks: TaskProvider<out Jar>,
    body: Jar.() -> Unit = {},
): TaskProvider<Jar> {
    val sourcesJarTask = sourcesJar(body)

    sourcesJarTask.configure {
        val archiveOperations = serviceOf<ArchiveOperations>()
        embeddedDepSourcesJarTasks.forEach { embeddedSourceJarTask ->
            dependsOn(embeddedSourceJarTask)
            from(embeddedSourceJarTask.map { archiveOperations.zipTree(it.archiveFile) })
        }
    }

    return sourcesJarTask
}

@JvmOverloads
fun Jar.addEmbeddedSources(configurationName: String = "embedded") {
    project.configurations.findByName(configurationName)?.let { embedded ->
        val allSources by lazy {
            embedded.resolvedConfiguration
                .resolvedArtifacts
                .map { it.id.componentIdentifier }
                .filterIsInstance<ProjectComponentIdentifier>()
                .mapNotNull {
                    project.project(it.projectPath).sources()
                }
        }
        from({ allSources })
    }
}

@JvmOverloads
fun Project.javadocJar(body: Jar.() -> Unit = {}): TaskProvider<Jar> {
    configure<JavaPluginExtension> {
        withJavadocJar()
    }

    val javadocTask = getOrCreateTask<Jar>("javadocJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("javadoc")
        tasks.findByName("javadoc")?.let { it as Javadoc }?.takeIf { it.enabled }?.let {
            dependsOn(it)
            from(it.destinationDir)
        }
        body()
    }

    addArtifact("archives", javadocTask)

    configurePublishedComponent {
        addVariantsFromConfiguration(configurations[JAVADOC_ELEMENTS_CONFIGURATION_NAME]) { }
    }

    return javadocTask
}

/**
 * Also embeds into final '-javadoc.jar' file javadoc files from embedded dependencies.
 */
fun Project.javadocJarWithJavadocFromEmbedded(
    vararg embeddedDepJavadocJarTasks: TaskProvider<out Jar>,
    body: Jar.() -> Unit = {},
): TaskProvider<Jar> {
    val javadocJarTask = javadocJar(body)

    javadocJarTask.configure {
        val archiveOperations = serviceOf<ArchiveOperations>()
        embeddedDepJavadocJarTasks.forEach { embeddedJavadocJarTask ->
            dependsOn(embeddedJavadocJarTask)
            from(embeddedJavadocJarTask.map { archiveOperations.zipTree(it.archiveFile) })
        }
    }

    return javadocJarTask
}


fun Project.standardPublicJars() {
    runtimeJar()
    sourcesJar()
    javadocJar()
}

@JvmOverloads
fun Project.publish(moduleMetadata: Boolean = false, sbom: Boolean = true, configure: MavenPublication.() -> Unit = { }) {
    apply<KotlinBuildPublishingPlugin>()

    if (!moduleMetadata) {
        tasks.withType<GenerateModuleMetadata> {
            enabled = false
        }
    }

    val publication = extensions.findByType<PublishingExtension>()
        ?.publications
        ?.findByName(mainPublicationName) as MavenPublication
    publication.configure()
    if (sbom) {
        configureSbom()
    }
}

fun Project.idePluginDependency(block: () -> Unit) {
    val shouldActivate = rootProject.findProperty("publish.ide.plugin.dependencies")?.toString()?.toBoolean() == true
    if (shouldActivate) {
        block()
    }
}

fun Project.publishJarsForIde(projects: List<String>, libraryDependencies: List<String> = emptyList()) {
    idePluginDependency {
        publishProjectJars(projects, libraryDependencies)
    }
    configurations.all {
        // Don't allow `ideaIC` from compiler to leak into Kotlin plugin modules. Compiler and
        // plugin may depend on different versions of IDEA and it will lead to version conflict
        exclude(module = ideModuleName())
    }
    dependencies {
        projects.forEach {
            jpsLikeJarDependency(project(it), JpsDepScope.COMPILE, { isTransitive = false }, exported = true)
        }
        libraryDependencies.forEach {
            jpsLikeJarDependency(it, JpsDepScope.COMPILE, exported = true)
        }
    }
}

fun Project.publishTestJarsForIde(projectNames: List<String>) {
    idePluginDependency {
        // Compiler test infrastructure should not affect test running in IDE.
        // If required, the components should be registered on the IDE plugin side.
        val excludedPaths = listOf("junit-platform.properties", "META-INF/services/**/*")
        publishTestJar(projectNames, excludedPaths)
    }
    configurations.all {
        // Don't allow `ideaIC` from compiler to leak into Kotlin plugin modules. Compiler and
        // plugin may depend on different versions of IDEA and it will lead to version conflict
        exclude(module = ideModuleName())
    }
    dependencies {
        for (projectName in projectNames) {
            jpsLikeJarDependency(projectTests(projectName), JpsDepScope.COMPILE, exported = true)
        }
    }
}

fun Project.publishProjectJars(projects: List<String>, libraryDependencies: List<String> = emptyList()) {
    apply<JavaPlugin>()

    val fatJarContents by configurations.creating

    dependencies {
        for (projectName in projects) {
            fatJarContents(project(projectName)) { isTransitive = false }
        }

        for (libraryDependency in libraryDependencies) {
            fatJarContents(libraryDependency) { isTransitive = false }
        }
    }

    publish()

    val jar: Jar by tasks

    jar.apply {
        dependsOn(fatJarContents)
        val archiveOperations = project.serviceOf<ArchiveOperations>()
        from {
            fatJarContents.map(archiveOperations::zipTree)
        }
    }

    sourcesJar {
        for (projectPath in projects) {
            val projectTasks = project(projectPath).tasks
            if (projectTasks.names.any { it == "compileKotlin" }) {
                // this is needed in order to declare explicit dependency on code generation tasks
                dependsOn(projectTasks.named("compileKotlin").map { it.dependsOn })
            }
        }
        from {
            projects.map {
                project(it).mainSourceSet.allSource
            }
        }
    }

    javadocJar()
}

fun Project.publishTestJar(projects: List<String>, excludedPaths: List<String>) {
    apply<JavaPlugin>()

    val fatJarContents by configurations.creating

    dependencies {
        for (projectName in projects) {
            fatJarContents(project(projectName, configuration = "tests-jar")) { isTransitive = false }
        }
    }

    publish(sbom = false)

    val jar: Jar by tasks

    jar.apply {
        dependsOn(fatJarContents)
        val archiveOperations = project.serviceOf<ArchiveOperations>()
        from {
            fatJarContents.map(archiveOperations::zipTree)
        }

        exclude(excludedPaths)
    }

    sourcesJar {
        from {
            projects.map { project(it).testSourceSet.allSource }
        }
    }

    javadocJar()
}

fun ConfigurationContainer.getOrCreate(name: String): Configuration = findByName(name) ?: create(name)

fun Jar.setupPublicJar(
    baseName: String,
    classifier: String = ""
) = setupPublicJar(
    project.provider { baseName },
    project.provider { classifier }
)

fun Jar.setupPublicJar(
    baseName: Provider<String>,
    classifier: Provider<String> = project.provider { "" }
) {
    val buildNumber = project.rootProject.extra["buildNumber"] as String
    this.archiveBaseName.set(baseName)
    this.archiveClassifier.set(classifier)
    manifest.attributes.apply {
        put("Implementation-Vendor", "JetBrains")
        put("Implementation-Title", baseName.get())
        put("Implementation-Version", buildNumber)
    }
}

fun Project.addArtifact(configuration: Configuration, task: Task, artifactRef: Any, body: ConfigurablePublishArtifact.() -> Unit = {}) {
    artifacts.add(configuration.name, artifactRef) {
        builtBy(task)
        body()
    }
}

fun Project.addArtifact(configurationName: String, task: Task, artifactRef: Any, body: ConfigurablePublishArtifact.() -> Unit = {}) =
    addArtifact(configurations.getOrCreate(configurationName), task, artifactRef, body)

fun <T : Task> Project.addArtifact(
    configurationName: String,
    task: TaskProvider<T>,
    body: ConfigurablePublishArtifact.() -> Unit = {}
): PublishArtifact {
    configurations.maybeCreate(configurationName)
    return artifacts.add(configurationName, task, body)
}

fun <T : Task> Project.addArtifact(
    configurationName: String,
    task: TaskProvider<T>,
    artifactRef: Any,
    body: ConfigurablePublishArtifact.() -> Unit = {}
): PublishArtifact {
    configurations.maybeCreate(configurationName)
    return artifacts.add(configurationName, artifactRef) {
        builtBy(task)
        body()
    }
}

fun Project.cleanArtifacts() {
    configurations["archives"].artifacts.let { artifacts ->
        artifacts.forEach {
            artifacts.remove(it)
        }
    }
}

fun Project.configurePublishedComponent(configure: AdhocComponentWithVariants.() -> Unit) =
    (components.findByName(KotlinBuildPublishingPlugin.ADHOC_COMPONENT_NAME) as AdhocComponentWithVariants?)?.apply(configure)
