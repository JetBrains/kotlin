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

val appcodeVersion = rootProject.extra["versions.appcode"] as String
val appcodeVersionRepo = rootProject.extra["versions.appcode.repo"] as String

dependencies {
    compile(project(":kotlin-ultimate:cidr-native"))
    compile(project(":idea:idea-gradle-native"))
    compileOnly(tc("$appcodeVersionRepo:$appcodeVersion:unscrambled/appcode.jar"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
