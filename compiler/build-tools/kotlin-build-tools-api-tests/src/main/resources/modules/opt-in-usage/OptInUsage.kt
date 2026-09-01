@RequiresOptIn(message = "This API is experimental and requires opt-in")
annotation class MyExperimentalApi

@MyExperimentalApi
fun experimentalApi() {}

fun useExperimentalApi() {
    experimentalApi()
}
