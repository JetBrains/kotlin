// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface My

internal class Your: My

// Code is valid, despite of delegate is internal
class His: My by Your()
