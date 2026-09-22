/*
How to Publish

1. Bump version parameter
2. Prepare publication credentials for https://packages.jetbrains.team/maven/p/kt/kotlin-dependencies
3. Execute `./gradlew -p dependencies/protobuf publish -PkotlinSpaceUsername=usr -PkotlinSpacePassword=token`
 */

val protobufVersion = "2.6.1"
extra.set("protobufVersion", protobufVersion)
val publishedVersion = "2.6.1-2"
extra.set("publishedVersion", publishedVersion)

allprojects {
    group = "org.jetbrains.kotlin"
    version = publishedVersion

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}
