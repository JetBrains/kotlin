package net.server


import require
import net.server.handlers.AbstractHandler

/**
 * Created by user on 7/27/16.
 */

fun start(handlers: MutableMap<String, AbstractHandler>, port:Int) {
    val http = require("http")
    val url = require("url")
    http.createServer({ request, response ->
        val content = mutableListOf<Byte>()
        val urlName = url.parse(request.url).pathname;
        val handler = handlers.get(urlName)
        request.on("data", {
            data ->
            for (i in 0..data.length - 1) {
                content.add(data[i])
            }
        })
        request.on("end", {
            if (handler != null) {
                handler.execute(content, response)
            } else {
                //todo write error on incorrect url
                response.end()
            }
        })
    }).listen(port)
}