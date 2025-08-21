/*
How to Publish

1. Bump version parameter
2. Prepare publication credentials for https://jetbrains.team/p/kt/packages/maven/kotlin-dependencies
3. Execute `./gradlew -p dependencies/protobuf publish -PkotlinSpaceUsername=usr -PkotlinSpacePassword=token`
 */

val protobufVersion by extra("4.29.3")
val publishedVersion by extra("4.29.3-1")

allprojects {
    group = "org.jetbrains.kotlin"
    version = publishedVersion

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}