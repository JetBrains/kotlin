object Control {
    val BLINK_DURATION = 1000

    fun run() {
        Memory.setHeap(Memory.DYNAMIC_HEAP)
        while (true) {
            executeCommand()
            Memory.cleanDynamicHeap()
        }
    }

    private fun executeCommand() {
        val task = Reader.readTask()
        when (task.type.id) {
            TaskRequest.Type.DEBUG.id -> debugTask()
            TaskRequest.Type.ROUTE.id -> routeTask()
            TaskRequest.Type.ROUTE_METRIC.id -> routeMetricTask()
            TaskRequest.Type.SONAR.id -> sonarTask()
            TaskRequest.Type.SONAR_EXPLORE.id -> sonarExploreTask()
        }
    }

    private fun debugTask() {
        val request = Reader.readDebug()

        when (request.type.id) {
            DebugRequest.Type.MEMORY_STATS.id -> sendMemoryStats()
        }
    }

    private fun routeTask() {
        val route = Reader.readRoute()

        val times = route.times
        val directions = route.directions
        var i = 0

        while (i < times.size) {
            val time = times[i]
            val direction = directions[i]

            Engine.go(direction)
            Time.wait(time)
            Engine.stop()

            i++
        }

        val response = RouteResponse.BuilderRouteResponse(0).build()
        Writer.writeRoute(response)
    }

    private fun routeMetricTask() {
        val route = Reader.readMetricRoute()
        val distances = route.distances
        var i = 0

        while (i < distances.size) {
            val distance = distances[i]
            val direction = route.directions[i]
            Engine.drive(direction, distance)

            i++
        }

        val response = RouteResponse.BuilderRouteResponse(0).build()
        Writer.writeRoute(response)
    }

    private fun sonarTask() {
        val request = Reader.readSonar()
        val angles = request.angles
        val size = angles.size
        val attempts = request.attempts
        val distances = IntArray(size)

        var i = 0
        while (i < size) {
            distances[i] = sonarMeasure(request.smoothing, attempts[i], angles[i], request.windowSize)
            i++
        }

        val response = SonarResponse.BuilderSonarResponse(distances).build()
        Writer.writeSonar(response)
    }

    private fun sonarExploreTask() {
        val request = Reader.readSonarExplore()

        val angle = request.angle
        val window = request.windowSize
        val result = Sonar.getRange(max(angle - window, 0), min(angle + window, 360))

        val response = SonarExploreAngleResponse.BuilderSonarExploreAngleResponse(result).build()
        Writer.writeSonarExplore(response)
    }

    private fun sonarMeasure(smoothing: SonarRequest.Smoothing, attempts: Int, angle: Int, windowSize: Int): Int {
        val data = IntArray(attempts)
        var i = 0
        while (i < attempts) {
            data[i] = Sonar.getSmoothDistance(angle, windowSize, smoothing)

            if (smoothing.id == SonarRequest.Smoothing.NONE.id && data[i] != -1) {
                return data[i]
            }
            i++
        }

        return when (smoothing.id) {
            SonarRequest.Smoothing.MEAN.id -> data.filter(::positive).mean()
            SonarRequest.Smoothing.MEDIAN.id -> data.filter(::positive).median()
            else -> -1
        }
    }

    private fun sendMemoryStats() {
        val stats = DebugResponseMemoryStats.BuilderDebugResponseMemoryStats(
                DebugInfo.getDynamicHeapTail(),
                DebugInfo.getStaticHeapTail(),
                DebugInfo.getDynamicHeapMaxSize(),
                DebugInfo.getDynamicHeapTotalBytes()
        ).build()

        Writer.writeMemoryStats(stats)
    }
}
