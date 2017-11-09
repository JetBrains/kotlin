inline fun <reified T : Any> uninitializedEntry(): T {
    val klass = T::class.java
    if (klass.isInterface) {
        throw RuntimeException()
    }
    else {
        return klass.newInstance()
    }
}

class ItemType

object ItemTypes {
    @JvmField
    val IRON_SHOVEL: ItemType = uninitializedEntry()

    @JvmField
    val IRON_PICKAXE: ItemType = uninitializedEntry()

    @JvmField
    val IRON_AXE: ItemType = uninitializedEntry()

    @JvmField
    val FLINT_AND_STEEL: ItemType = uninitializedEntry()

    @JvmField
    val APPLE: ItemType = uninitializedEntry()

    @JvmField
    val ARROW: ItemType = uninitializedEntry()

    @JvmField
    val COAL: ItemType = uninitializedEntry()

    @JvmField
    val DIAMOND: ItemType = uninitializedEntry()

    @JvmField
    val IRON_INGOT: ItemType = uninitializedEntry()

    @JvmField
    val GOLD_INGOT: ItemType = uninitializedEntry()

    @JvmField
    val IRON_SWORD: ItemType = uninitializedEntry()

    @JvmField
    val WOODEN_SWORD: ItemType = uninitializedEntry()

    @JvmField
    val WOODEN_SHOVEL: ItemType = uninitializedEntry()

    @JvmField
    val WOODEN_PICKAXE: ItemType = uninitializedEntry()

    @JvmField
    val WOODEN_AXE: ItemType = uninitializedEntry()

    @JvmField
    val STONE_SWORD: ItemType = uninitializedEntry()

    @JvmField
    val STONE_SHOVEL: ItemType = uninitializedEntry()

    @JvmField
    val STONE_PICKAXE: ItemType = uninitializedEntry()

    @JvmField
    val STONE_AXE: ItemType = uninitializedEntry()

    @JvmField
    val DIAMOND_SWORD: ItemType = uninitializedEntry()

    @JvmField
    val DIAMOND_SHOVEL: ItemType = uninitializedEntry()

    @JvmField
    val DIAMOND_PICKAXE: ItemType = uninitializedEntry()

    @JvmField
    val DIAMOND_AXE: ItemType = uninitializedEntry()

    @JvmField
    val STICK: ItemType = uninitializedEntry()

    @JvmField
    val BOWL: ItemType = uninitializedEntry()

    @JvmField
    val MUSHROOM_STEW: ItemType = uninitializedEntry()

    @JvmField
    val GOLDEN_SWORD: ItemType = uninitializedEntry()

    @JvmField
    val GOLDEN_SHOVEL: ItemType = uninitializedEntry()

    @JvmField
    val GOLDEN_PICKAXE: ItemType = uninitializedEntry()

    @JvmField
    val GOLDEN_AXE: ItemType = uninitializedEntry()

    @JvmField
    val STRING: ItemType = uninitializedEntry()

    @JvmField
    val GUNPOWDER: ItemType = uninitializedEntry()

    @JvmField
    val WOODEN_HOE: ItemType = uninitializedEntry()

    @JvmField
    val STONE_HOE: ItemType = uninitializedEntry()

    @JvmField
    val IRON_HOE: ItemType = uninitializedEntry()

    @JvmField
    val SEEDS: ItemType = uninitializedEntry()

    @JvmField
    val WHEAT: ItemType = uninitializedEntry()

    @JvmField
    val BREAD: ItemType = uninitializedEntry()

    @JvmField
    val LEATHER_CAP: ItemType = uninitializedEntry()

    @JvmField
    val LEATHER_TUNIC: ItemType = uninitializedEntry()

    @JvmField
    val LEATHER_PANTS: ItemType = uninitializedEntry()

    @JvmField
    val LEATHER_BOOTS: ItemType = uninitializedEntry()

    @JvmField
    val CHAIN_HELMET: ItemType = uninitializedEntry()

    @JvmField
    val CHAIN_CHESTPLATE: ItemType = uninitializedEntry()

    @JvmField
    val CHAIN_LEGGINGS: ItemType = uninitializedEntry()

    @JvmField
    val CHAIN_BOOTS: ItemType = uninitializedEntry()

    @JvmField
    val IRON_HELMET: ItemType = uninitializedEntry()

    @JvmField
    val IRON_CHESTPLATE: ItemType = uninitializedEntry()

    @JvmField
    val IRON_LEGGINGS: ItemType = uninitializedEntry()

    @JvmField
    val IRON_BOOTS: ItemType = uninitializedEntry()

    @JvmField
    val GOLDEN_HELMET: ItemType = uninitializedEntry()

    @JvmField
    val GOLDEN_CHESTPLATE: ItemType = uninitializedEntry()

    @JvmField
    val GOLDEN_LEGGINGS: ItemType = uninitializedEntry()

    @JvmField
    val GOLDEN_BOOTS: ItemType = uninitializedEntry()

    @JvmField
    val FLINT: ItemType = uninitializedEntry()

    @JvmField
    val RAW_PORKCHOP: ItemType = uninitializedEntry()

    @JvmField
    val COOKED_PORKCHOP: ItemType = uninitializedEntry()

    @JvmField
    val PAINTING: ItemType = uninitializedEntry()

    @JvmField
    val GOLDEN_APPLE: ItemType = uninitializedEntry()

    @JvmField
    val SIGN: ItemType = uninitializedEntry()

    @JvmField
    val WOODEN_DOOR: ItemType = uninitializedEntry()

    @JvmField
    val BUCKET: ItemType = uninitializedEntry()

    @JvmField
    val MINECART: ItemType = uninitializedEntry()

    @JvmField
    val SADDLE: ItemType = uninitializedEntry()

    @JvmField
    val IRON_DOOR: ItemType = uninitializedEntry()

    @JvmField
    val REDSTONE: ItemType = uninitializedEntry()

    @JvmField
    val SNOWBALL: ItemType = uninitializedEntry()

    @JvmField
    val BOAT: ItemType = uninitializedEntry()

    @JvmField
    val LEATHER: ItemType = uninitializedEntry()

    @JvmField
    val BRICK: ItemType = uninitializedEntry()

    @JvmField
    val CLAY: ItemType = uninitializedEntry()

    @JvmField
    val SUGAR_CANE: ItemType = uninitializedEntry()

    @JvmField
    val PAPER: ItemType = uninitializedEntry()

    @JvmField
    val BOOK: ItemType = uninitializedEntry()

    @JvmField
    val SLIMEBALL: ItemType = uninitializedEntry()

    @JvmField
    val MINECART_WITH_CHEST: ItemType = uninitializedEntry()

    @JvmField
    val EGG: ItemType = uninitializedEntry()

    @JvmField
    val COMPASS: ItemType = uninitializedEntry()

    @JvmField
    val FISHING_ROD: ItemType = uninitializedEntry()

    @JvmField
    val CLOCK: ItemType = uninitializedEntry()

    @JvmField
    val GLOWSTONE_DUST: ItemType = uninitializedEntry()

    @JvmField
    val RAW_FISH: ItemType = uninitializedEntry()

    @JvmField
    val COOKED_FISH: ItemType = uninitializedEntry()

    @JvmField
    val DYE: ItemType = uninitializedEntry()

    @JvmField
    val BONE: ItemType = uninitializedEntry()

    @JvmField
    val SUGAR: ItemType = uninitializedEntry()

    @JvmField
    val CAKE: ItemType = uninitializedEntry()

    @JvmField
    val BED: ItemType = uninitializedEntry()

    @JvmField
    val REDSTONE_REPEATER: ItemType = uninitializedEntry()

    @JvmField
    val COOKIE: ItemType = uninitializedEntry()

    @JvmField
    val FILLED_MAP: ItemType = uninitializedEntry()

    @JvmField
    val SHEARS: ItemType = uninitializedEntry()

    @JvmField
    val MELON: ItemType = uninitializedEntry()

    @JvmField
    val PUMPKIN_SEEDS: ItemType = uninitializedEntry()

    @JvmField
    val MELON_SEEDS: ItemType = uninitializedEntry()

    @JvmField
    val RAW_BEEF: ItemType = uninitializedEntry()

    @JvmField
    val STEAK: ItemType = uninitializedEntry()

    @JvmField
    val RAW_CHICKEN: ItemType = uninitializedEntry()

    @JvmField
    val COOKED_CHICKEN: ItemType = uninitializedEntry()

    @JvmField
    val ROTTEN_FLESH: ItemType = uninitializedEntry()

    @JvmField
    val BLAZE_ROD: ItemType = uninitializedEntry()

    @JvmField
    val GHAST_TEAR: ItemType = uninitializedEntry()

    @JvmField
    val GOLD_NUGGET: ItemType = uninitializedEntry()

    @JvmField
    val NETHER_WART: ItemType = uninitializedEntry()

    @JvmField
    val POTION: ItemType = uninitializedEntry()

    @JvmField
    val GLASS_BOTTLE: ItemType = uninitializedEntry()

    @JvmField
    val SPIDER_EYE: ItemType = uninitializedEntry()

    @JvmField
    val FERMENTED_SPIDER_EYE: ItemType = uninitializedEntry()

    @JvmField
    val BLAZE_POWDER: ItemType = uninitializedEntry()

    @JvmField
    val MAGMA_CREAM: ItemType = uninitializedEntry()

    @JvmField
    val BREWING_STAND: ItemType = uninitializedEntry()

    @JvmField
    val CAULDRON: ItemType = uninitializedEntry()

    @JvmField
    val GLISTERING_MELON: ItemType = uninitializedEntry()

    @JvmField
    val SPAWN_EGG: ItemType = uninitializedEntry()

    @JvmField
    val BOTTLE_O_ENCHANTING: ItemType = uninitializedEntry()

    @JvmField
    val FIRE_CHARGE: ItemType = uninitializedEntry()

    @JvmField
    val EMERALD: ItemType = uninitializedEntry()

    @JvmField
    val ITEM_FRAME: ItemType = uninitializedEntry()

    @JvmField
    val FLOWER_POT: ItemType = uninitializedEntry()

    @JvmField
    val CARROT: ItemType = uninitializedEntry()

    @JvmField
    val POTATO: ItemType = uninitializedEntry()

    @JvmField
    val BAKED_POTATO: ItemType = uninitializedEntry()

    @JvmField
    val POISONOUS_POTATO: ItemType = uninitializedEntry()

    @JvmField
    val EMPTY_MAP: ItemType = uninitializedEntry()

    @JvmField
    val GOLDEN_CARROT: ItemType = uninitializedEntry()

    @JvmField
    val MOB_HEAD: ItemType = uninitializedEntry()

    @JvmField
    val CARROT_ON_A_STICK: ItemType = uninitializedEntry()

    @JvmField
    val PUMPKIN_PIE: ItemType = uninitializedEntry()

    @JvmField
    val ENCHANTED_BOOK: ItemType = uninitializedEntry()

    @JvmField
    val REDSTONE_COMPARATOR: ItemType = uninitializedEntry()

    @JvmField
    val NETHER_BRICK: ItemType = uninitializedEntry()

    @JvmField
    val NETHER_QUARTZ: ItemType = uninitializedEntry()

    @JvmField
    val MINECART_WITH_TNT: ItemType = uninitializedEntry()

    @JvmField
    val MINECART_WITH_HOPPER: ItemType = uninitializedEntry()

    @JvmField
    val HOPPER: ItemType = uninitializedEntry()

    @JvmField
    val RAW_RABBIT: ItemType = uninitializedEntry()

    @JvmField
    val COOKED_RABBIT: ItemType = uninitializedEntry()

    @JvmField
    val RABBIT_STEW: ItemType = uninitializedEntry()

    @JvmField
    val RABBIT_FOOT: ItemType = uninitializedEntry()

    @JvmField
    val RABBIT_HIDE: ItemType = uninitializedEntry()

    @JvmField
    val LEATHER_HORSE_ARMOR: ItemType = uninitializedEntry()

    @JvmField
    val IRON_HORSE_ARMOR: ItemType = uninitializedEntry()

    @JvmField
    val GOLDEN_HORSE_ARMOR: ItemType = uninitializedEntry()

    @JvmField
    val DIAMOND_HORSE_ARMOR: ItemType = uninitializedEntry()

    @JvmField
    val LEAD: ItemType = uninitializedEntry()

    @JvmField
    val NAME_TAG: ItemType = uninitializedEntry()

    @JvmField
    val RAW_MUTTON: ItemType = uninitializedEntry()

    @JvmField
    val SPRUCE_DOOR: ItemType = uninitializedEntry()

    @JvmField
    val BIRCH_DOOR: ItemType = uninitializedEntry()

    @JvmField
    val JUNGLE_DOOR: ItemType = uninitializedEntry()

    @JvmField
    val ACACIA_DOOR: ItemType = uninitializedEntry()

    @JvmField
    val DARK_OAK_DOOR: ItemType = uninitializedEntry()

    @JvmField
    val SPLASH_POTION: ItemType = uninitializedEntry()

    @JvmField
    val BEETROOT: ItemType = uninitializedEntry()

    @JvmField
    val BEETROOT_SEEDS: ItemType = uninitializedEntry()

    @JvmField
    val BEETROOT_SOUP: ItemType = uninitializedEntry()

    @JvmField
    val RAW_SALMON: ItemType = uninitializedEntry()

    @JvmField
    val CLOWNFISH: ItemType = uninitializedEntry()

    @JvmField
    val PUFFERFISH: ItemType = uninitializedEntry()

    @JvmField
    val COOKED_SALMON: ItemType = uninitializedEntry()

    @JvmField
    val ENCHANTED_GOLDEN_APPLE: ItemType = uninitializedEntry()

    /////////////////// BLOCKS ///////////////////

    @JvmField
    val STONE: ItemType = uninitializedEntry()

    @JvmField
    val GRASS_BLOCK: ItemType = uninitializedEntry()

    @JvmField
    val DIRT: ItemType = uninitializedEntry()

    @JvmField
    val COBBLESTONE: ItemType = uninitializedEntry()

    @JvmField
    val WOOD_PLANKS: ItemType = uninitializedEntry()

    @JvmField
    val SAPLING: ItemType = uninitializedEntry()

    @JvmField
    val BEDROCK: ItemType = uninitializedEntry()

    @JvmField
    val WATER_BUCKET: ItemType = uninitializedEntry()

    @JvmField
    val LAVA_BUCKET: ItemType = uninitializedEntry()

    @JvmField
    val SAND: ItemType = uninitializedEntry()

    @JvmField
    val GRAVEL: ItemType = uninitializedEntry()

    @JvmField
    val GOLD_ORE: ItemType = uninitializedEntry()

    @JvmField
    val IRON_ORE: ItemType = uninitializedEntry()

    @JvmField
    val COAL_ORE: ItemType = uninitializedEntry()

    @JvmField
    val WOOD: ItemType = uninitializedEntry()

    @JvmField
    val LEAVES: ItemType = uninitializedEntry()

    @JvmField
    val SPONGE: ItemType = uninitializedEntry()

    @JvmField
    val GLASS: ItemType = uninitializedEntry()

    @JvmField
    val LAPIS_LAZULI_ORE: ItemType = uninitializedEntry()

    @JvmField
    val LAPIS_LAZULI_BLOCK: ItemType = uninitializedEntry()

    @JvmField
    val DISPENSER: ItemType = uninitializedEntry()

    @JvmField
    val SANDSTONE: ItemType = uninitializedEntry()

    @JvmField
    val NOTE_BLOCK: ItemType = uninitializedEntry()

    @JvmField
    val DETECTOR_RAIL: ItemType = uninitializedEntry()

    @JvmField
    val STICKY_PISTON: ItemType = uninitializedEntry()

    @JvmField
    val COBWEB: ItemType = uninitializedEntry()

    @JvmField
    val TALL_GRASS: ItemType = uninitializedEntry()

    @JvmField
    val DEAD_BUSH: ItemType = uninitializedEntry()

    @JvmField
    val WOOL: ItemType = uninitializedEntry()

    @JvmField
    val YELLOW_FLOWER: ItemType = uninitializedEntry()

    @JvmField
    val RED_FLOWER: ItemType = uninitializedEntry()

    @JvmField
    val BROWN_MUSHROOM: ItemType = uninitializedEntry()

    @JvmField
    val RED_MUSHROOM: ItemType = uninitializedEntry()

    @JvmField
    val BLOCK_OF_GOLD: ItemType = uninitializedEntry()

    @JvmField
    val BLOCK_OF_IRON: ItemType = uninitializedEntry()

    @JvmField
    val DOUBLE_STONE_SLAB: ItemType = uninitializedEntry()

    @JvmField
    val STONE_SLAB: ItemType = uninitializedEntry()

    @JvmField
    val BRICKS: ItemType = uninitializedEntry()

    @JvmField
    val TNT: ItemType = uninitializedEntry()

    @JvmField
    val BOOKSHELF: ItemType = uninitializedEntry()

    @JvmField
    val MOSS_STONE: ItemType = uninitializedEntry()

    @JvmField
    val OBSIDIAN: ItemType = uninitializedEntry()

    @JvmField
    val TORCH: ItemType = uninitializedEntry()

    @JvmField
    val FIRE: ItemType = uninitializedEntry()

    @JvmField
    val MOB_SPAWNER: ItemType = uninitializedEntry()

    @JvmField
    val OAK_WOOD_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val CHEST: ItemType = uninitializedEntry()

    @JvmField
    val DIAMOND_ORE: ItemType = uninitializedEntry()

    @JvmField
    val BLOCK_OF_DIAMOND: ItemType = uninitializedEntry()

    @JvmField
    val CRAFTING_TABLE: ItemType = uninitializedEntry()

    @JvmField
    val FARMLAND: ItemType = uninitializedEntry()

    @JvmField
    val FURNACE: ItemType = uninitializedEntry()

    @JvmField
    val LADDER: ItemType = uninitializedEntry()

    @JvmField
    val RAIL: ItemType = uninitializedEntry()

    @JvmField
    val COBBLESTONE_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val LEVER: ItemType = uninitializedEntry()

    @JvmField
    val STONE_PRESSURE_PLATE: ItemType = uninitializedEntry()

    @JvmField
    val WOODEN_PRESSURE_PLATE: ItemType = uninitializedEntry()

    @JvmField
    val REDSTONE_ORE: ItemType = uninitializedEntry()

    @JvmField
    val GLOWING_REDSTONE_ORE: ItemType = uninitializedEntry()

    @JvmField
    val REDSTONE_TORCH: ItemType = uninitializedEntry()

    @JvmField
    val STONE_BUTTON: ItemType = uninitializedEntry()

    @JvmField
    val SNOW_LAYER: ItemType = uninitializedEntry()

    @JvmField
    val ICE: ItemType = uninitializedEntry()

    @JvmField
    val SNOW: ItemType = uninitializedEntry()

    @JvmField
    val CACTUS: ItemType = uninitializedEntry()

    @JvmField
    val FENCE: ItemType = uninitializedEntry()

    @JvmField
    val PUMPKIN: ItemType = uninitializedEntry()

    @JvmField
    val NETHERRACK: ItemType = uninitializedEntry()

    @JvmField
    val SOUL_SAND: ItemType = uninitializedEntry()

    @JvmField
    val GLOWSTONE: ItemType = uninitializedEntry()

    @JvmField
    val JACK_O_LANTERN: ItemType = uninitializedEntry()

    @JvmField
    val TRAPDOOR: ItemType = uninitializedEntry()

    @JvmField
    val MONSTER_EGG: ItemType = uninitializedEntry()

    @JvmField
    val STONE_BRICK: ItemType = uninitializedEntry()

    @JvmField
    val IRON_BARS: ItemType = uninitializedEntry()

    @JvmField
    val GLASS_PANE: ItemType = uninitializedEntry()

    @JvmField
    val VINE: ItemType = uninitializedEntry()

    @JvmField
    val FENCE_GATE: ItemType = uninitializedEntry()

    @JvmField
    val BRICK_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val STONE_BRICK_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val MYCELIUM: ItemType = uninitializedEntry()

    @JvmField
    val LILY_PAD: ItemType = uninitializedEntry()

    @JvmField
    val NETHER_BRICK_FENCE: ItemType = uninitializedEntry()

    @JvmField
    val NETHER_BRICK_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val ENCHANTMENT_TABLE: ItemType = uninitializedEntry()

    @JvmField
    val END_PORTAL_FRAME: ItemType = uninitializedEntry()

    @JvmField
    val END_STONE: ItemType = uninitializedEntry()

    @JvmField
    val REDSTONE_LAMP: ItemType = uninitializedEntry()

    @JvmField
    val DROPPER: ItemType = uninitializedEntry()

    @JvmField
    val ACTIVATOR_RAIL: ItemType = uninitializedEntry()

    @JvmField
    val COCOA: ItemType = uninitializedEntry()

    @JvmField
    val SANDSTONE_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val EMERALD_ORE: ItemType = uninitializedEntry()

    @JvmField
    val TRIPWIRE_HOOK: ItemType = uninitializedEntry()

    @JvmField
    val BLOCK_OF_EMERALD: ItemType = uninitializedEntry()

    @JvmField
    val SPRUCE_WOOD_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val BIRCH_WOOD_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val JUNGLE_WOOD_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val COBBLESTONR_WALL: ItemType = uninitializedEntry()

    @JvmField
    val WOODEN_BUTTON: ItemType = uninitializedEntry()

    @JvmField
    val ANVIL: ItemType = uninitializedEntry()

    @JvmField
    val TRAPPED_CHEST: ItemType = uninitializedEntry()

    @JvmField
    val WEIGHTED_PRESSURE_PLATE_LIGHT: ItemType = uninitializedEntry()

    @JvmField
    val WEIGHTED_PRESSURE_PLATE_HEAVY: ItemType = uninitializedEntry()

    @JvmField
    val DAYLIGHT_SENSOR: ItemType = uninitializedEntry()

    @JvmField
    val BLOCK_OF_REDSTONE: ItemType = uninitializedEntry()

    @JvmField
    val NETHER_QUARTZ_ORE: ItemType = uninitializedEntry()

    @JvmField
    val BLOCK_OF_QUARTZ: ItemType = uninitializedEntry()

    @JvmField
    val QUARTZ_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val WOODEN_SLAB: ItemType = uninitializedEntry()

    @JvmField
    val STAINED_CLAY: ItemType = uninitializedEntry()

    @JvmField
    val ACACIA_LEAVES: ItemType = uninitializedEntry()

    @JvmField
    val ACACIA_WOOD: ItemType = uninitializedEntry()

    @JvmField
    val ACACIA_WOOD_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val DARK_OAK_WOOD_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val SLIME_BLOCK: ItemType = uninitializedEntry()

    @JvmField
    val IRON_TRAPDOOR: ItemType = uninitializedEntry()

    @JvmField
    val HEY_BALE: ItemType = uninitializedEntry()

    @JvmField
    val CARPET: ItemType = uninitializedEntry()

    @JvmField
    val HARDENED_CLAY: ItemType = uninitializedEntry()

    @JvmField
    val BLOCK_OF_COAL: ItemType = uninitializedEntry()

    @JvmField
    val PACKED_ICE: ItemType = uninitializedEntry()

    @JvmField
    val SUNFLOWER: ItemType = uninitializedEntry()

    @JvmField
    val INVERTED_DAYLIGHT_SENSOR: ItemType = uninitializedEntry()

    @JvmField
    val RED_SANDSTONE: ItemType = uninitializedEntry()

    @JvmField
    val RED_SANDSTONE_STAIRS: ItemType = uninitializedEntry()

    @JvmField
    val RED_SANDSTONE_SLAB: ItemType = uninitializedEntry()

    @JvmField
    val SPRUCE_FENCE_GATE: ItemType = uninitializedEntry()

    @JvmField
    val BIRCH_FENCE_GATE: ItemType = uninitializedEntry()

    @JvmField
    val JUNGLE_FENCE_GATE: ItemType = uninitializedEntry()

    @JvmField
    val DARK_OAK_FENCE_GATE: ItemType = uninitializedEntry()

    @JvmField
    val ACACIA_FENCE_GATE: ItemType = uninitializedEntry()

    @JvmField
    val JUNGLE_GOOR: ItemType = uninitializedEntry()

    @JvmField
    val GRASS_PATH: ItemType = uninitializedEntry()

    @JvmField
    val PODZOL: ItemType = uninitializedEntry()
}

// 0 GETSTATIC