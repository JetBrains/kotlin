package jet

public abstract class Enum<E: Enum<E>>(name: String, ordinal: Int) {
  public final fun name()    : String
  public final fun ordinal() : Int
}
