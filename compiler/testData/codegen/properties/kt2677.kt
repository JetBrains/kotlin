class U<T>

open class WeatherReport
{
    public open var forecast: U<String> = U<String>()
}

open class DerivedWeatherReport() : WeatherReport()
{
    public override var forecast: U<String>
        get() = super.forecast
        set(newv: U<String>) { super.forecast = newv }
}