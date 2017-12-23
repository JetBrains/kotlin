// !WITH_NEW_INFERENCE
// See KT-15566
// NI_EXPECTED_FILE

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

private fun <T> lazy(init: () -> T): kotlin.<!UNRESOLVED_REFERENCE!>Lazy<!><T> {
    init()
    null!!
}

object DefaultHttpClientWithBy : HttpClient by client {
    val client by lazy { HttpClientImpl() }
}

object DefaultFqHttpClient : HttpClient by DefaultFqHttpClient.<!UNINITIALIZED_VARIABLE!>client<!> {
    val client = HttpClientImpl()
}