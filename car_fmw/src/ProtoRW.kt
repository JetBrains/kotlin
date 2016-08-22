object Reader {
    fun readRoute(): RouteRequest {
        val stream = getInputStream()
        return RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0)).parseFrom(stream).build()
    }

    fun readTask(): TaskRequest {
        val stream = getInputStream()
        return TaskRequest.BuilderTaskRequest(TaskRequest.Type.DEBUG).parseFrom(stream).build()
    }

    fun readDebug(): DebugRequest {
        val stream = getInputStream()
        return DebugRequest.BuilderDebugRequest(DebugRequest.Type.MEMORY_STATS).parseFrom(stream).build()
    }

    fun readSonar(): SonarRequest {
        val stream = getInputStream()
        return SonarRequest.BuilderSonarRequest(IntArray(0)).parseFrom(stream).build()
    }

    private fun getInputStream(): CodedInputStream {
        val buffer = Connection.receiveByteArray()
        return CodedInputStream(buffer)
    }
}

object Writer {
    fun writeRoute(route: RouteResponse) {
        val stream = getOutputStream(route.getSizeNoTag())
        route.writeTo(stream)
        Connection.sendByteArray(stream.buffer)
    }

    fun writeMemoryStats(stats: DebugResponseMemoryStats) {
        val stream = getOutputStream(stats.getSizeNoTag())
        stats.writeTo(stream)
        Connection.sendByteArray(stream.buffer)
    }

    fun writeSonar(response: SonarResponse) {
        val stream = getOutputStream(response.getSizeNoTag())
        response.writeTo(stream)
        Connection.sendByteArray(stream.buffer)
    }

    private fun getOutputStream(size: Int): CodedOutputStream {
        val buffer = ByteArray(size)
        return CodedOutputStream(buffer)
    }
}