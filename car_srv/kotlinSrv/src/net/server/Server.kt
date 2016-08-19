package net.server


import net.server.handlers.AbstractHandler
import require

fun start(handlers: MutableMap<String, AbstractHandler>, port: Int) {
    val http = require("http")
    val url = require("url")
    http.createServer({ request, response ->
        val content = mutableListOf<Byte>()
        val urlName = url.parse(request.url).pathname
        val handler = handlers[urlName]
        request.on("data", {
            data ->
            for (i in 0..data.length - 1) {
                content.add(data[i])
            }
        })
        request.on("end", {
            if (handler != null) {
                try {
                    handler.execute(content, response)
                } catch (e: dynamic) {
                    response.end()
                }
            } else {
                response.end()
            }
        })
    }).listen(port)
}