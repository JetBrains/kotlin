// FILE: a.jet

package weatherForecast

fun weatherToday() = "snow"

// FILE: b.jet

package myApp

import weatherForecast.weatherToday
import weatherForecast.weatherToday

fun needUmbrella() = weatherToday() == "rain"
