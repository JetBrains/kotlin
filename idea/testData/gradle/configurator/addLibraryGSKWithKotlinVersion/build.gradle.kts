val kotlin_version: String by extra
dependencies {
    testCompile("junit:junit:4.12")
    compile(kotlinModule("stdlib-jre8", kotlin_version))
}
