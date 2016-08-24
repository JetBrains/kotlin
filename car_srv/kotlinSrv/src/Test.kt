import control.emulator.ControllerEmulator
import room.Room

fun runTests() {
    testCarEmulator()
}

private fun testCarEmulator() {
    val controller = ControllerEmulator()
    var sonarRequest = SonarRequest.BuilderSonarRequest(IntArray(2, { x -> x * 90 })).build()
    var sonarResponse = SonarResponse.BuilderSonarResponse(IntArray(0)).build()
    controller.executeRequestSensorData(sonarRequest, { bytes ->
        sonarResponse.mergeFrom(CodedInputStream(bytes))
        val dist0 = sonarResponse.distances[0]
        val dist90 = sonarResponse.distances[1]
        eq(dist0.toDouble(), Math.abs(Room.rightLine.C / Room.rightLine.A), "check angle 0", 2.5)
        eq(dist90.toDouble(), Room.bottomLine.C / Room.bottomLine.B, "check angle 90", 2.5)
    })

    println("--------------------")
    sonarRequest = SonarRequest.BuilderSonarRequest(IntArray(2, { 120 + 60 * it })).build()
    sonarResponse = SonarResponse.BuilderSonarResponse(IntArray(0)).build()
    controller.executeRequestSensorData(sonarRequest, { bytes ->
        sonarResponse.mergeFrom(CodedInputStream(bytes))
        val dist120 = sonarResponse.distances[0]
        val dist180 = sonarResponse.distances[1]
        eq(dist120.toDouble(), 23.094, "check angle 120", 2.5)
        eq(dist180.toDouble(), 150.0, "check angle 180", 2.5)
    })

    println("--------------------")
    CarState.instance.x = 50
    CarState.instance.y = 50
    CarState.instance.angle = 90
    sonarRequest = SonarRequest.BuilderSonarRequest(IntArray(3, { x -> x * 90 })).build()
    sonarResponse = SonarResponse.BuilderSonarResponse(IntArray(0)).build()
    controller.executeRequestSensorData(sonarRequest, { bytes ->
        sonarResponse.mergeFrom(CodedInputStream(bytes))
        val dist0 = sonarResponse.distances[0]
        val dist90 = sonarResponse.distances[1]
        val dist180 = sonarResponse.distances[2]
        eq(dist0.toDouble(), 250.0, "check angle 0", 2.5)
        eq(dist90.toDouble(), 150.0, "check angle 90", 2.5)
        eq(dist180.toDouble(), 70.0, "check angle 180", 2.5)
    })

    println("--------------------")
    CarState.instance.x = 50
    CarState.instance.y = 50
    CarState.instance.angle = 720 + 45
    sonarRequest = SonarRequest.BuilderSonarRequest(IntArray(1, { x -> 90 })).build()
    sonarResponse = SonarResponse.BuilderSonarResponse(IntArray(0)).build()
    controller.executeRequestSensorData(sonarRequest, { bytes ->
        sonarResponse.mergeFrom(CodedInputStream(bytes))
        val dist90 = sonarResponse.distances[0]
        eq(dist90.toDouble(), 98.9949, "check angle 90", 2.5)
    })
}

private var testNum = 1

private fun eq(value1: Double, value2: Double, msg: String, eps: Double) {
    if (Math.abs(value1 - value2) > eps) {
        println("test failed! $msg")
        println("returned: $value1")
        println("waited: $value2")
    } else {
        println("test ${testNum++} ok!")
    }
}
