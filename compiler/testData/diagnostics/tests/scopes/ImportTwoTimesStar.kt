// FILE: a.jet

package weatherForecast

fun weatherToday() = "snow"

// FILE: b.jet

package myApp

import weatherForecast.*
import weatherForecast.*

fun needUmbrella() = weatherToday() == "rain"
