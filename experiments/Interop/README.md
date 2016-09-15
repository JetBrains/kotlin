# Kotlin-native interop

## Preparation

Go to `../kni` and build it as described:

```
$ ant -f update_dependencies.xml
$ ant
```

## Usage

Open `Interop` project in IDEA. Run `StubGenerator` configuration to process all `.def` files in `Example` module.
Then run `Example` configuration to run the example.
