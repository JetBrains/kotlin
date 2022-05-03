import gradle.kotlin.dsl.accessors._6fcc03dc3ac2d481a1d2799d95ed6983.implementation
import org.gradle.kotlin.dsl.dependencies

// Contains common configuration that should be applied to all projects

// Forcing minimal gson dependency version
val gsonVersion = rootProject.extra["versions.gson"] as String
dependencies {
    constraints {
        configurations.all {
            this@constraints.add(name, "com.google.code.gson:gson") {
                version {
                    require(gsonVersion)
                }
                because("Force using same gson version because of https://github.com/google/gson/pull/1991")
            }
        }
    }
}