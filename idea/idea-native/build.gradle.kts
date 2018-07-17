import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc

plugins {
    kotlin("jvm")
    //application
    id("com.github.jk1.tcdeps") version "0.17"
}



repositories {
    teamcityServer {
        setUrl("http://buildserver.labs.intellij.net")
        credentials {
            username = "guest"
            password = "guest"
        }
    }
}

val kotlinNativeVersion = "0.9-dev-2859"

dependencies {
    compile(tc("Kotlin_KotlinNative_Master_KotlinNativeLinuxDist:$kotlinNativeVersion:shared.jar"))
    compile(tc("Kotlin_KotlinNative_Master_KotlinNativeLinuxDist:$kotlinNativeVersion:backend.native.jar"))

    compile(project(":idea"))
    compile(project(":idea:idea-core"))
    compile(project(":compiler:frontend"))
    compileOnly(intellijDep())
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureInstrumentation()

runtimeJar {
    archiveName = "native-ide.jar"
}
