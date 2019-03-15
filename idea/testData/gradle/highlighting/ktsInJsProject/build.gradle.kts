import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    id("kotlin2js") version "1.3.10"
}

dependencies {
    implementation(kotlin("stdlib-js", "1.3.10"))
}

repositories {
    jcenter()
}

tasks {
    "compileKotlin2Js"(Kotlin2JsCompile::class) {
        kotlinOptions {
            metaInfo = true
        }
    }
}
