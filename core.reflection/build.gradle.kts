
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.assembler.tasks.Assemble
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.org.objectweb.asm.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath(files("$rootDir/ideaSDK/lib/asm-all.jar"))
    }
}

apply {
    plugin("kotlin")
    plugin("com.github.johnrengelman.shadow")
}

fun Jar.setupRuntimeJar(implementationTitle: String): Unit {
    dependsOn(":prepare:build.version:prepare")
    manifest.attributes.apply {
        put("Built-By", rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Vendor", rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Title", implementationTitle)
        put("Implementation-Version", rootProject.extra["build.number"])
    }
    from(configurations.getByName("build-version").files) {
        into("META-INF/")
    }
}

fun DependencyHandler.buildVersion(): Dependency {
    val cfg = configurations.create("build-version")
    return add(cfg.name, project(":prepare:build.version", configuration = "default"))
}

fun commonDep(coord: String): String {
    val parts = coord.split(':')
    return when (parts.size) {
        1 -> "$coord:$coord:${rootProject.extra["versions.$coord"]}"
        2 -> "${parts[0]}:${parts[1]}:${rootProject.extra["versions.${parts[1]}"]}"
        3 -> coord
        else -> throw IllegalArgumentException("Illegal maven coordinates: $coord")
    }
}

fun commonDep(group: String, artifact: String): String = "$group:$artifact:${rootProject.extra["versions.$artifact"]}"

val protobufLiteProject = ":custom-dependencies:protobuf-lite"
fun KotlinDependencyHandler.protobufLite(): ProjectDependency =
        project(protobufLiteProject, configuration = "default").apply { isTransitive = false }
val protobufLiteTask = "$protobufLiteProject:prepare"

// TODO: common ^ 8< ----

// Set to false to prevent relocation and metadata stripping on kotlin-reflect.jar and reflection sources. Use to debug reflection
val obfuscateReflect = true

val additionalDepsCfg = configurations.create("additionalDeps")

//val mainCfg = configurations.create("default1")

val outputReflectPreJarFileBase = "$buildDir/libs/kotlin-reflect-pre"

val outputReflectJarFileBase = "$buildDir/libs/kotlin-reflect"

//artifacts.add(mainCfg.name, File(outputReflectJarFileBase + ".jar"))

repositories {
    mavenLocal()
    maven { setUrl(rootProject.extra["repo"]) }
}

configure<JavaPluginConvention> {
    sourceSets.getByName("main")?.apply {
        val srcs = listOf(File(rootDir, "core/reflection.jvm/src"))
        java.setSrcDirs(srcs)
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

dependencies {
    compile(project(":core.builtins"))
    compile(project(":core"))
    compile(project(":libraries:stdlib"))
    compile(protobufLite())
    buildVersion()
    additionalDepsCfg.name(commonDep("javax.inject"))
    additionalDepsCfg.name(protobufLite())
}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask)
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask)
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", "kotlin-reflection")
}

/*

val prePackReflectTask = task<ShadowJar>("pre-pack-reflect") {
    this.enabled = true
    dependsOn("classes")
    classifier = "beforeStrip"
//    classifier = if (obfuscateReflect) outputReflectJarFileBase + "_beforeStrip" else outputReflectJarFileBase
//    configurations = listOf(mainCfg)
    this.configurations = listOf(project.configurations.getByName("default"))
    setupRuntimeJar("Kotlin Reflect")
    from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
    from(project(":core").the<JavaPluginConvention>().sourceSets.getByName("main").output)
    from(project(":core").file("descriptor.loader.java/src")) {
        include("META-INF/services*/
/**")
    }
    from(additionalDepsCfg.files)
    manifest.attributes.put("Class-Path", "kotlin-runtime.jar")

    if (obfuscateReflect) {
        relocate("org.jetbrains.kotlin", "kotlin.reflect.jvm.internal.impl")
        relocate("javax.inject", "kotlin.reflect.jvm.internal.impl.javax.inject")
    }
}

val mainTask = task("prepare") {
    dependsOn(prePackReflectTask)
    val inFile = File(outputReflectJarFileBase + "_beforeStrip.jar")
    val outFile = File(outputReflectJarFileBase + ".jar")
    val annotationRegex = "kotlin/Metadata".toRegex()
    val classRegex = "kotlin/reflect/jvm/internal/impl/.*".toRegex()
    doLast {
        println("Stripping annotations from all classes in $inFile")
        println("Input file size: ${inFile.length()} bytes")

        fun transform(entryName: String, bytes: ByteArray): ByteArray {
            if (!entryName.endsWith(".class")) return bytes
            if (!classRegex.matches(entryName.removeSuffix(".class"))) return bytes

            var changed = false
            val classWriter = ClassWriter(0)
            val classVisitor = object : ClassVisitor(Opcodes.ASM5, classWriter) {
                override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                    if (annotationRegex.matches(Type.getType(desc).getInternalName())) {
                        changed = true
                        return null
                    }
                    return super.visitAnnotation(desc, visible)
                }
            }
            ClassReader(bytes).accept(classVisitor, 0)
            if (!changed) return bytes

            return classWriter.toByteArray()
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use {
            outJar ->
            val inJar = JarFile(inFile)
            try {
                for (entry in inJar.entries()) {
                    val inBytes = inJar.getInputStream(entry).readBytes()
                    val outBytes = transform(entry.getName(), inBytes)

                    if (inBytes.size < outBytes.size) {
                        error("Size increased for ${entry.getName()}: was ${inBytes.size} bytes, became ${outBytes.size} bytes")
                    }

                    entry.setCompressedSize(-1L)
                    outJar.putNextEntry(entry)
                    outJar.write(outBytes)
                    outJar.closeEntry()
                }
            }
            finally {
                // Yes, JarFile does not extend Closeable on JDK 6 so we can't use "use" here
                inJar.close()
            }
        }

        println("Output written to $outFile")
        println("Output file size: ${outFile.length()} bytes")
    }
}
*/

task("sourcesets") {
    doLast {
        the<JavaPluginConvention>().sourceSets.all {
            println("--> ${it.name}: ${it.java.srcDirs.joinToString()}")
        }
    }
}

//tasks.withType<Assemble> {
//    dependsOn(prePackReflectTask.name)
//}
