package net.server.handlers

import CodedOutputStream
import DebugResponseMemoryStats
import DirectionResponse
import LocationResponse
import RouteDoneRequest
import RouteRequest
import RouteResponse
import UploadResult

abstract class AbstractHandler {

    fun execute(data: List<Byte>, response: dynamic) {
        getBytesResponse(data.toByteArray(), { resultBytes ->
            val resultBuffer = js("new Buffer(resultBytes)")
            response.write(resultBuffer)
            response.end()
        })
    }


    abstract fun getBytesResponse(data: ByteArray, callback: (b: ByteArray) -> Unit)

    protected fun <T> encodeProtoBuf(protoMessage: T): ByteArray {
        val routeBytes: ByteArray
        if (protoMessage is LocationResponse) {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        } else if (protoMessage is UploadResult) {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        } else if (protoMessage is DebugResponseMemoryStats) {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        } else if (protoMessage is DirectionResponse) {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        } else if (protoMessage is RouteResponse) {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        } else if (protoMessage is RouteRequest) {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        } else if (protoMessage is RouteDoneRequest) {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        } else {
            println("PROTO MESSAGE DON'T ENCODE!")
            routeBytes = ByteArray(0)
        }
        return routeBytes
    }

}