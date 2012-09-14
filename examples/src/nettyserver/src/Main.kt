package org.jetbrains.kotlin.examples.netty

import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.buffer.ChannelBuffers
import java.nio.charset.Charset

fun main(args : Array<String>) {
    if(args.size == 0) {
        println("Please provide command line argument <path to static resources>")
        System.exit(1)
    }
    else {
        println("Please open http://localhost:8080/index.html page in your browser")
    }

    httpServer(8080) {
        static(directory = args[0])

        rest("/sayhello") {
            GET {
                response.content = "Hello, World!"
            }
            POST {
                response.content = "You said: ${request.getContent()!!.toString(Charset.defaultCharset())}"
            }
        }

        onNotFound = { requestResponse ->
            when(requestResponse.path) {
                "/prototype.js" -> {
                    requestResponse.redirect("https://ajax.googleapis.com/ajax/libs/prototype/1.7.0.0/prototype.js")
                }
                else ->
                    requestResponse.setError(HttpResponseStatus.FORBIDDEN)
            }
        }
    }
}
