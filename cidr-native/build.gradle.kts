import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc

plugins {
    kotlin("jvm")
    id("com.github.jk1.tcdeps") version "0.18"
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

val clionVersion = rootProject.extra["versions.clion"] as String
val clionVersionRepo = rootProject.extra["versions.clion.repo"] as String

dependencies {
    compile(project(":idea:idea-native"))
    compileOnly(tc("$clionVersionRepo:$clionVersion:unscrambled/clion.jar"))
    compile(intellijDep()) {
        includeJars("java-api", "java-impl")
        isTransitive = false
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
