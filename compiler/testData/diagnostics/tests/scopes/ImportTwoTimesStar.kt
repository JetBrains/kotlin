// FILE: a.kt

package weatherForecast

fun weatherToday() = "snow"

// FILE: b.kt

package myApp

import weatherForecast.*
import weatherForecast.*

fun needUmbrella() = weatherToday() == "rain"
