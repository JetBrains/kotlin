helm uninstall remote-kotlin-daemon
helm install remote-kotlin-daemon oci://registry.jetbrains.team/p/cb/helm-charts/simple-app -f compiler/daemon/remote-daemon/k8s/values.yaml
// removed liveness and readiness