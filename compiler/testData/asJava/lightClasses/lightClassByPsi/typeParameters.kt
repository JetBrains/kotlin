interface I

interface I2<T>
interface I3<T : I>
interface I4<T : I?>

interface I5<Self : I5<Self>>
interface I6<Self : I6<Self>?>
interface I7<Self : I7<Self?>?>
interface I8<Self : I8<Self?>>