package org.jetbrains.kotlin.examples.netty

import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpResponseStatus

class RestBuilder() {
    var onGet  : (RequestResponse.()->Any?)? = null
    var onPost : (RequestResponse.()->Any?)? = null

    fun GET(handler: RequestResponse.()->Unit) {
        onGet = handler
    }

    fun POST(handler: RequestResponse.()->Unit) {
        onPost = handler
    }
}

class RestProcessor(val prefix: String, val builder: RestBuilder) : Processor {
    override fun tryToProcess(request: RequestResponse): Boolean {
        if(request.path.startsWith(prefix)) {
            if(request.request.getMethod() == HttpMethod.GET && builder.onGet != null) {
                request.ok()
                request.(builder.onGet.sure())()
            }
            else if(request.request.getMethod() == HttpMethod.POST && builder.onPost != null) {
                request.ok()
                request.(builder.onPost.sure())()
            }
            else {
                request.setError(HttpResponseStatus.METHOD_NOT_ALLOWED)
            }
            return true;
        }
        return false
    }
}