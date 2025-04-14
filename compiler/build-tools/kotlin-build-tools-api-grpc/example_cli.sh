grpcurl -plaintext \
        -import-path ./src/main/proto \
        -proto bta.proto \
        -d '{
          "compilerArguments": {
            "javaSources": ["abc"]
          }
        }' \
        localhost:8081 \
        org.jetbrains.bta.JvmToolchain/compile