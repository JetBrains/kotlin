// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

import java.net.URI

fun <T> WebClient.myPost(uri: URI, body: Any, extract: WebClient.ResponseSpec.() -> Mono<T>): Mono<T> = TODO()

class RestClient(private val webClient: WebClient) {
    fun post(outDto: OutDto): Mono<InDto> =
        webClient.myPost(URI("http:/localhost:8080"), outDto) { bodyToMono() }
}

class Mono<T>

class WebClient {
    fun post() {}

    interface ResponseSpec {
        fun <T> bodyToMono(): Mono<T>
    }
}

class OutDto
class InDto