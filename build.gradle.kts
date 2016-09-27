
extra["kotlinVersion"] = file("kotlin-version-for-gradle.txt").readText().trim()
extra["repo"] = "https://repo.gradle.org/gradle/repo"

//buildscript {
//    //extra["kotlinVersion"] = "1.1-M01"
//    extra["kotlinVersion"] = file("kotlin-version-for-gradle.txt").readText().trim()
//    extra["repo"] = "https://repo.gradle.org/gradle/repo"
//
//    repositories {
//        mavenLocal()
//        maven { setUrl(extra["repo"]) }
//    }
//
//    dependencies {
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra["kotlinVersion"]}")
//        classpath("org.jetbrains.kotlin:kotlin-compiler-embeddable:${extra["kotlinVersion"]}")
//    }
//}
//
//apply { plugin("kotlin") }
