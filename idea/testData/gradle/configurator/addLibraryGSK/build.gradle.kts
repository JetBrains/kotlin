plugins {
    kotlin("jvm") version "{{kotlin_plugin_version}}"
}

dependencies {
    testCompile("junit:junit:4.12")
    compile(kotlin("stdlib-jre8"))
}
