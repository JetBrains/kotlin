package org.jetbrains.kotlin.examples.netty

import org.jboss.netty.handler.codec.http.HttpMethod
import java.io.File
import java.io.RandomAccessFile
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.channel.DefaultFileRegion
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpResponseStatus.*

class StaticFileProcessor(val prefix: String, val root: String) : Processor {
    override fun tryToProcess(request: RequestResponse) : Boolean {
        if(request.path.startsWith(prefix)) {
            return request.processStaticFile()
        }
        return false;
    }

    fun RequestResponse.processStaticFile() : Boolean {
        val file = File(root + File.separator + path.substring(prefix.length))
        if(!file.exists())
            return false;

        if (request.getMethod() != HttpMethod.GET) {
            setError(METHOD_NOT_ALLOWED);
            return true;
        }

        if (file.isHidden() || !file.isFile()) {
            setError(FORBIDDEN)
            return true
        }

        var raf = RandomAccessFile(file, "r")

        val fileLength = raf.length()

        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, OK)
        response["Content-Length"] = fileLength

        channel.write(response)
        channel.write(DefaultFileRegion(raf.getChannel(), 0, fileLength))
        return true;
    }
}

