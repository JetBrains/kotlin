plugins {
    kotlin("jvm")
}

group = "org.jetbrains.kotlin"
version = "2.2.255-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.register("consumeHeap") {
    this.doFirst {
        val heapConsumer = mutableListOf<String>()
        for (i in 1..Int.MAX_VALUE) {
            heapConsumer.add("string$i".repeat(100))
        }
        println(heapConsumer.last())
    }
}