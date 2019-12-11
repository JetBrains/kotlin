interface My

internal class Your: My

// Code is valid, despite of delegate is internal
class His: My by Your()
