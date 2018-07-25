class ImpulsMigration
{
    fun migrate(oldVersion: Long)
    {
        var _oldVersion = oldVersion

        if (4 > 3)
        {
            _oldVersion = 1
        }

        if (1 > 3)
        {
            _oldVersion++
        }
    }
}

fun box(): String {
    ImpulsMigration().migrate(1L)
    return "OK"
}
