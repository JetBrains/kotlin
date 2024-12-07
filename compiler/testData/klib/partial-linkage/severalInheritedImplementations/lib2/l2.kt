package lib2

import lib1.*

class I_Default : I, Default
class Default_I : Default, I
class I_RemovedDefault : I, RemovedDefault
class RemovedDefault_I : RemovedDefault, I
class I_J_Default : I, J, Default
class Default_J_I : Default, I, J
class I_J_RemovedDefault : I, J, RemovedDefault
class RemovedDefault_J_I : RemovedDefault, I, J


class Unrelated_I_Default : Unrelated(), I, Default
class Unrelated_Default_I : Unrelated(), Default, I
class Unrelated_I_RemovedDefault : Unrelated(), I, RemovedDefault
class Unrelated_RemovedDefault_I : Unrelated(), RemovedDefault, I
class Unrelated_I_J_Default : Unrelated(), I, J, Default
class Unrelated_Default_J_I : Unrelated(), Default, I, J
class Unrelated_I_J_RemovedDefault : Unrelated(), I, J, RemovedDefault
class Unrelated_RemovedDefault_J_I : Unrelated(), RemovedDefault, I, J

class AbstractUnrelated_I_Default : AbstractUnrelated(), I, Default
class AbstractUnrelated_Default_I : AbstractUnrelated(), Default, I
class AbstractUnrelated_I_RemovedDefault : AbstractUnrelated(), I, RemovedDefault
class AbstractUnrelated_RemovedDefault_I : AbstractUnrelated(), RemovedDefault, I
class AbstractUnrelated_I_J_Default : AbstractUnrelated(), I, J, Default
class AbstractUnrelated_Default_J_I : AbstractUnrelated(), Default, I, J
class AbstractUnrelated_I_J_RemovedDefault : AbstractUnrelated(), I, J, RemovedDefault
class AbstractUnrelated_RemovedDefault_J_I : AbstractUnrelated(), RemovedDefault, I, J


class WithFakeOverride_I_Default : WithFakeOverride(), I, Default
class WithFakeOverride_Default_I : WithFakeOverride(), Default, I
class WithFakeOverride_I_RemovedDefault : WithFakeOverride(), I, RemovedDefault
class WithFakeOverride_RemovedDefault_I : WithFakeOverride(), RemovedDefault, I
class WithFakeOverride_I_J_Default : WithFakeOverride(), I, J, Default
class WithFakeOverride_Default_J_I : WithFakeOverride(), Default, I, J
class WithFakeOverride_I_J_RemovedDefault : WithFakeOverride(), I, J, RemovedDefault
class WithFakeOverride_RemovedDefault_J_I : WithFakeOverride(), RemovedDefault, I, J

class WithRealOverride_I_Default : WithRealOverride(), I, Default
class WithRealOverride_Default_I : WithRealOverride(), Default, I
class WithRealOverride_I_RemovedDefault : WithRealOverride(), I, RemovedDefault
class WithRealOverride_RemovedDefault_I : WithRealOverride(), RemovedDefault, I
class WithRealOverride_I_J_Default : WithRealOverride(), I, J, Default
class WithRealOverride_Default_J_I : WithRealOverride(), Default, I, J
class WithRealOverride_I_J_RemovedDefault : WithRealOverride(), I, J, RemovedDefault
class WithRealOverride_RemovedDefault_J_I : WithRealOverride(), RemovedDefault, I, J
