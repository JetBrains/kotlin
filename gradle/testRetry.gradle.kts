
apply(plugin = "org.gradle.test-retry")

tasks.withType<Test>().configureEach {
    configure<org.gradle.testretry.TestRetryTaskExtension> {
        maxRetries.set(if (kotlinBuildProperties.isTeamcityBuild) 3 else 0)
        maxFailures.set(20)
        failOnPassedAfterRetry.set(false)
    }
}