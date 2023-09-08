// FIR_IDENTICAL
// ISSUE: KT-61459

interface Alarm {
    interface Builder<Self : Builder<Self>> {
        fun build(): Alarm
    }
}

abstract class AbstractAlarm<Self : AbstractAlarm<Self, Builder>, Builder : AbstractAlarm.Builder<Builder, Self>>(
    val identifier: String,
) : Alarm {
    abstract class Builder<Self : Builder<Self, Built>, Built : AbstractAlarm<Built, Self>> : Alarm.Builder<Self> {
        private var identifier: String = ""

        fun setIdentifier(text: String): Self {
            this.identifier = text
            return this <!UNCHECKED_CAST!>as Self<!>
        }

        final override fun build(): Built = TODO()
    }
}
