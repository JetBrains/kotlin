val kotlin_version: String by extra
buildscript {
    var kotlin_version: String by extra
    kotlin_version = "$VERSION$"
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlinModule("gradle-plugin", kotlin_version))
    }
}
apply {
    plugin("kotlin-android")
}
dependencies {
    compile(kotlinModule("stdlib-jre7", kotlin_version))
}
repositories {
    mavenCentral()
}