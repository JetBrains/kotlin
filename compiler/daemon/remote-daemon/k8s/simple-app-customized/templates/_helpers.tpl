{{- define "helm.fullname" -}}
{{- .Values.applicationName | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "helm.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "helm.labels" -}}
helm.sh/chart: {{ include "helm.chart" . }}
{{ include "helm.selectorLabels" . }}
{{- end }}

{{- define "helm.selectorLabels" -}}
app: {{ include "helm.fullname" . }}
{{- end }}

{{- define "helm.domainName" }}{{ .Values.applicationName }}.{{ .Values.domainName }}{{- end }}

{{- define "helm.tlsSecretName" }}{{ .Values.applicationName | replace "." "-" }}-{{ .Values.domainName | replace "." "-" }}-tls{{- end }}

# https://kubernetes.io/docs/reference/labels-annotations-taints/#topologykubernetesiozone
{{- define "helm.affinity" }}
podAntiAffinity:
  preferredDuringSchedulingIgnoredDuringExecution:
    - podAffinityTerm:
        labelSelector:
          matchExpressions:
          - key: app
            operator: In
            values:
            - {{ .Values.applicationName | trunc 63 | trimSuffix "-" }}
        topologyKey: topology.kubernetes.io/zone
      weight: 100
    - podAffinityTerm:
        labelSelector:
          matchExpressions:
          - key: app
            operator: In
            values:
            - {{ .Values.applicationName | trunc 63 | trimSuffix "-" }}
        topologyKey: kubernetes.io/hostname
      weight: 10
{{- end }}

{{- define "helm.env" }}

  {{- if gt (len .Values.customEnv) 0 }}
    {{- range $i, $env := .Values.customEnv }}
- name: {{ $env.name }}
  {{- if $env.valueFrom }}
  valueFrom:
    {{ toYaml $env.valueFrom | nindent 14 }}
  {{- else }}
  value: {{ $env.value | quote }}
  {{- end }}
    {{- end }}
  {{- end }}
  {{- if gt (len .Values.databaseSecrets) 0 }}
    {{- range $i, $secretName := .Values.databaseSecrets }}
- name: DB{{$i}}_URL
  valueFrom:
    secretKeyRef:
      name: {{ $secretName }}
      key: db_url
      optional: true
- name: DB{{$i}}_URL_JDBC
  valueFrom:
    secretKeyRef:
      name: {{ $secretName }}
      key: db_url_jdbc
      optional: true
- name: DB{{$i}}_HOST
  valueFrom:
    secretKeyRef:
      name: {{ $secretName }}
      key: host
      optional: true
- name: DB{{$i}}_PORT
  valueFrom:
    secretKeyRef:
      name: {{ $secretName }}
      key: port
      optional: true
- name: DB{{$i}}_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ $secretName }}
      key: username
      optional: true
- name: DB{{$i}}_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ $secretName }}
      key: password
      optional: true
- name: DB{{$i}}_NAME
  valueFrom:
    secretKeyRef:
      name: {{ $secretName }}
      key: name
      optional: true
- name: DB{{$i}}_PARAMS
  valueFrom:
    secretKeyRef:
      name: {{ $secretName }}
      key: params
      optional: true
    {{- end }}
  {{- end }}
{{- end }}
