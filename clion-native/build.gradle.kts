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
    compile(project(":kotlin-ultimate:cidr-native"))
    compile(project(":idea:idea-gradle-native"))
    compileOnly(tc("$clionVersionRepo:$clionVersion:unscrambled/clion.jar"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
