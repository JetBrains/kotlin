// FILE: a.kt

package weatherForecast

fun weatherToday() = "snow"

// FILE: b.kt

package myApp

import weatherForecast.weatherToday
import weatherForecast.weatherToday

fun needUmbrella() = weatherToday() == "rain"
