
apply(plugin = "org.gradle.test-retry")

tasks.withType<Test>().configureEach {
    configure<org.gradle.testretry.TestRetryTaskExtension> {
        maxRetries.set(3)
        maxFailures.set(20)
        failOnPassedAfterRetry.set(false)
    }
}