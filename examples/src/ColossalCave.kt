package ColossalCave

import java.util.*
import java.io.*

open class Thing(val name : String) {
  open val ldesc : String get() = "This is a nondescript thing"
}

open class Room(name : String) : Thing(name) {
  open val west : Room? get() = null
  open val east : Room? get() = null
  open val north : Room? get() = null
  open val south : Room? get() = null
  var seen : Boolean = false
}

public val items : ArrayList<Item> = ArrayList<Item>()

open class Item(name : String, var room : Room?) : Thing(name) {
  open val isFixed : Boolean get() = false
  open val isListed : Boolean get() = true

  {
    items.add(this)
  }
}

open class FixedItem(name : String, room : Room?) : Item(name, room) {
  override val isFixed : Boolean get() = true
  override val isListed : Boolean get() = false
}

fun describeRoom(room : Room) {
  println(room.name)
  if (!room.seen) {
    println(room.ldesc)
    room.seen = true
  }
  for(val item in items) {
    if (item.room === room && item.isListed) {
      println("You see " + item.name)
    }
  }
}

object AtEndOfRoad: Room("At End of Road") {
  override val ldesc : String = "You are standing at the end of the road before a small brick building." +
  " Around you is a forest. A small stream flows out of the building and down a gully."

  override val west : Room? get() = AtHillInRoad
  override val east : Room? get() = InsideBuilding
  override val south : Room? get() = InAValley
}

object AtHillInRoad: Room("At Hill in Road") {
  override val ldesc : String = "You have walked up a hill, still in the forest. The road slopes back down " +
  "the other side of the hill. There is a building in the distance."

  override val east : Room? get() = AtEndOfRoad
}

val Hill = object: FixedItem("Hill", AtHillInRoad) {
  override val ldesc : String = "It's just a typical hill."
}

object InsideBuilding: Room("Inside Building") {
  override val ldesc : String = "You are inside a building, a well house for a large spring."

  override val west : Room? get() = AtEndOfRoad
}

object InAValley: Room("In A Valley") {
  override val ldesc : String = "You are in a valley in the forest beside a stream tumbling along a rocky bed."
  override val north : Room get() = AtEndOfRoad
  override val south : Room get() = AtSlitInStreambed
}

object AtSlitInStreambed: Room("At Slit In Streambed") {
  override val ldesc : String = "At your feet all the water of the stream splashs into a 2-inch slit in the rock." +
  " Downstream the streambed is bare rock."
  override val north : Room get() = InAValley
  override val south : Room get() = OutsideGrate
}

object OutsideGrate: Room("Outside Grate") {
  override val ldesc : String = "You are in a 20-foot depression floored with bare dirt. Set into the dirt " +
  "is a strong steel grate mounted in concrete. A dry streambed leads into the depression."
  override val north : Room get() = AtSlitInStreambed
}

val brassLantern = object: Item("Brass Lantern", InsideBuilding) {
}

val setOfKeys = object: Item("A Set of Keys", InsideBuilding) {
}

fun startRoom() = AtEndOfRoad

class Player(var room : Room) {
  val inventory : ArrayList<Item> = ArrayList<Item>()
}

abstract class Command {
  abstract fun execute(p : Player) : Unit
}

object QuitCommand : Command() {
  override fun execute(p : Player) {
    System.exit(0)
  }
}

abstract class MoveCommand : Command() {
  fun moveTo(p : Player, room : Room?) {
    if (room === null) {
      println("You can't go that way")
      return
    }
    p.room = room as Room
    describeRoom(p.room)
  }
}

object NorthCommand: MoveCommand() {
  override fun execute(p : Player) {
    moveTo(p, p.room.north)
  }
}

object SouthCommand: MoveCommand() {
  override fun execute(p : Player) {
    moveTo(p, p.room.south)
  }
}

object WestCommand: MoveCommand() {
  override fun execute(p : Player) {
    moveTo(p, p.room.west)
  }
}

object EastCommand: MoveCommand() {
  override fun execute(p : Player) {
    moveTo(p, p.room.east)
  }
}

abstract class CommandOnObject(val target : String) : Command() {
  override fun execute(p : Player) {
    for(val item in items) {
      if (item.name.equalsIgnoreCase(target) && item.room == p.room) {
        executeOn(p, item)
        return
      }
    }
    println("I don't see any " + target + " here.")
  }

  abstract fun executeOn(p : Player, item : Item)
}

class TakeCommand(target : String) : CommandOnObject(target) {
  override fun executeOn(p : Player, item : Item) {
    if (item.isFixed) {
      print("You can't have " + item.name)
      return
    }
    item.room = null
    p.inventory.add(item)
    println("Taken")
  }
}

class ExamineCommand(target : String) : CommandOnObject(target) {
  override fun executeOn(p : Player, item : Item) {
    println(item.ldesc)
  }
}

object InventoryCommand: Command() {
  override fun execute(p : Player) {
    if (p.inventory.size() == 0)
      println("You are empty-handed.")
    else {
      println("You are carrying:")
      for(val item in p.inventory) {
        println(item.name)
      }
    }
  }
}

fun parse(cmd : String) : Command? {
  if (cmd == "quit") return QuitCommand
  if (cmd == "north") return NorthCommand
  if (cmd == "south") return SouthCommand
  if (cmd == "west") return WestCommand
  if (cmd == "east") return EastCommand
  if (cmd == "inventory") return InventoryCommand
  if (cmd.startsWith("take")) {
    val target = cmd.substring(4).trim()
    return TakeCommand(target)
  }
  if (cmd.startsWith("examine")) {
    val target = cmd.substring(7).trim()
    return ExamineCommand(target)
  }
  return null
}

fun main(args : Array<String>) {
  val p = Player(startRoom())
  describeRoom(p.room)
  val reader = BufferedReader(InputStreamReader(System.`in`))
  while(true) {
    print("> ")
    val cmd = reader.readLine() as String
    val command = parse(cmd)
    if (command === null)
      println("Unrecognized command");
    else
      command.execute(p)
  }
}
