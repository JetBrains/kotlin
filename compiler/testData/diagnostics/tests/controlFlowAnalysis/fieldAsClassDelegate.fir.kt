// WITH_STDLIB
// NI_EXPECTED_FILE
// ISSUE: KT-15566, KT-56489

import DefaultHttpClient.client

interface HttpClient

class HttpClientImpl : HttpClient

// Below we should have initialization error for both (!) delegates

object DefaultHttpClient : HttpClient by <!UNINITIALIZED_VARIABLE!>client<!> {
    val client = HttpClientImpl()
}

object DefaultHttpClientWithGetter : HttpClient by client {
    val client get() = HttpClientImpl()
}

object DefaultHttpClientWithFun : HttpClient by fClient() {
}

private fun fClient() = HttpClientImpl()

object DefaultHttpClientWithBy : HttpClient by client {
    val client by lazy { HttpClientImpl() }
}

object DefaultFqHttpClient : HttpClient by <!UNINITIALIZED_VARIABLE!>DefaultFqHttpClient.client<!> {
    val client = HttpClientImpl()
}
