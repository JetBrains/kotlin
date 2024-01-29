// LANGUAGE: +ExpectedTypeGuidedResolution

data class Duration(val milliseconds: Int) {
    companion object {
        val Int.seconds: Duration get() = Duration(this * 1000)
        val Int.minutes: Duration get() = (this * 60).seconds
    }
}

val d: Duration = 1.seconds
