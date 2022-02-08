
apply(plugin = "org.gradle.test-retry")

val testRetryMaxRetries = findProperty("kotlin.build.testRetry.maxRetries")?.toString()?.toInt() ?:
    (if (kotlinBuildProperties.isTeamcityBuild) 3 else 0)

tasks.withType<Test>().configureEach {
    configure<org.gradle.testretry.TestRetryTaskExtension> {
        maxRetries.set(testRetryMaxRetries)
        maxFailures.set(20)
        failOnPassedAfterRetry.set(false)
    }
}