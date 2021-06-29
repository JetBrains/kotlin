@file:JvmName("JvmToolchain")
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.*
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

enum class JdkMajorVersion(
    val majorVersion: Int,
    private val mandatory: Boolean = true
) {
    JDK_1_6(6),
    JDK_1_7(7),
    JDK_1_8(8),
    JDK_9(9),
    JDK_10(10, false),
    JDK_11(11, false),
    JDK_15(15, false),
    JDK_16(16, false);

    fun isMandatory(): Boolean = mandatory
}

fun Project.configureJvmDefaultToolchain() {
    configureJvmToolchain(JdkMajorVersion.JDK_1_8)
}

fun Project.configureJvmToolchain(
    jdkVersion: JdkMajorVersion
) {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        val kotlinExtension = extensions.getByType<KotlinTopLevelExtension>()

        kotlinExtension.jvmToolchain {
            (this as JavaToolchainSpec).languageVersion
                .set(JavaLanguageVersion.of(jdkVersion.majorVersion))
        }

        tasks
            .matching { it.name != "compileJava9Java" && it is JavaCompile }
            .configureEach {
                with(this as JavaCompile) {
                    options.compilerArgs.add("-proc:none")
                    options.encoding = "UTF-8"
                }
            }

        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
        }
    }
}

fun Project.updateJvmTarget(
    jvmTarget: String
) {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = jvmTarget
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = jvmTarget
        targetCompatibility = jvmTarget
    }
}

fun Project.getToolchainCompilerFor(
    jdkVersion: JdkMajorVersion
): Provider<JavaCompiler> {
    val service = project.extensions.getByType<JavaToolchainService>()
    return service.compilerFor {
        this.languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
    }
}

fun Project.getToolchainLauncherFor(
    jdkVersion: JdkMajorVersion
): Provider<JavaLauncher> {
    val service = project.extensions.getByType<JavaToolchainService>()
    return service.launcherFor {
        this.languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
    }
}
